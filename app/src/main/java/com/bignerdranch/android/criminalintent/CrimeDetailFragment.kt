package com.bignerdranch.android.criminalintent

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import kotlin.contracts.Returns

private const val DATE_FORMAT = "EEE, MMM, dd"


class CrimeDetailFragment: Fragment() {

    private val args: CrimeDetailFragmentArgs by navArgs()
    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }


    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }
    //TESTING!!!!!!!!!!!!
    private var currentCrime: Crime? = null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentCrimeDetailBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }

            }

            //listener for suspect selection button
            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            //checks whether or not there is an appropriate contacts app to resolve request
            //disables button if null is returned
            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            //listener for the camera button, allows image capture through photo app
            crimeCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir,
                    photoName)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }

            //checks whether or not there is an appropriate camera app to resolve request
            //disables button if null is returned
            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                Uri.parse("")
            )
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)

            //Listener for the img icon NEEDS FIXING XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            crimePhoto.setOnClickListener {
                val crime = currentCrime ?: return@setOnClickListener
                val photoFileName = crime.photoFileName
                if (photoFileName.isNullOrEmpty()) return@setOnClickListener

                val photoFile = File(requireContext().filesDir, photoFileName)
                if (!photoFile.exists()) return@setOnClickListener

                val action = CrimeDetailFragmentDirections
                    .actionCrimeDetailFragmentToImageDetailFragment(photoFile.path)

                findNavController().navigate(action)
            }

        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let {
                        //TESTING!!!!
                        currentCrime= it
                        updateUi(it) }
                }
            }
        }
        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val newDate =
                bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = crime.date.toString()
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved

            //add listener for crime report button, uses Intent to find a program to send a message
            //with a title and crime details.
            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject)
                    )
                }
                //Force chooser to appear
                val chooserIntent = Intent.createChooser(
                    reportIntent,
                    getString(R.string.send_report)
                )
                // launch messaging intent with the chooser option forced
                startActivity(chooserIntent)

            }
            //displays the suspects name or default text if empty
            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }
            //displays the new crime bitmap image if it has changed
            updatePhoto(crime.photoFileName)


        }
    }
    //generate a report detailing all  crime related information handling the possible options
    // solved or not and suspect or not
    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString((R.string.crime_report_unsolved))
        }
        val dateString = DateFormat.format(DATE_FORMAT,crime.date)
        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(
            R.string.crime_report, crime.title, dateString, solvedString, suspectText
        )
    }
    //Allows for the return of a contacts name from a selected contact called though another app
    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    }
    //Ensures that a suitable app exists to select contacts from
    private fun canResolveIntent(intent: Intent): Boolean {
        //intent.addCategory(Intent.CATEGORY_HOME)
        //^^pretends there is no suitable contacts app
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    //allows for scaling of bitmap image to fit the required display size
    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }
            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
            }
        }
    }




    //waits for contact selection and then if that uri is not null it saves the suspect name
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { parseContactSelection(it) }
    }

    //waits for photo capture through camera app, if taken then stores the photo
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto && photoName != null) {
            crimeDetailViewModel.updateCrime { oldCrime ->
                oldCrime.copy(photoFileName = photoName)
            }
        }

    }

    private var photoName: String? = null





}